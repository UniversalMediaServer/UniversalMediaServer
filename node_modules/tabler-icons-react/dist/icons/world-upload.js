import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function WorldUpload(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-world-upload",
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
    d: "M21 12a9 9 0 1 0 -9 9"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3.6 9h16.8"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3.6 15h8.4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11.578 3a17 17 0 0 0 0 18"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12.5 3c1.719 2.755 2.5 5.876 2.5 9"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18 21v-7m3 3l-3 -3l-3 3"
  }));
}

export { WorldUpload as default };
