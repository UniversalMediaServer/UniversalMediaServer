import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function ToolsOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-tools-off",
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    stroke: color,
    strokeWidth: "2",
    fill: "none",
    strokeLinecap: "round",
    strokeLinejoin: "round"
  }, restProps), /*#__PURE__*/React.createElement("path", {
    stroke: "none",
    d: "M0 0h24v24H0z",
    fill: "none"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 12l4 -4a2.828 2.828 0 1 0 -4 -4l-4 4m-2 2l-7 7v4h4l7 -7"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14.5 5.5l4 4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 8l-5 -5m-2.004 2.004l-1.996 1.996l5 5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M7 8l-1.5 1.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 12l5 5m-2 2l-2 2l-5 -5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 17l-1.5 1.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { ToolsOff as default };
