import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Directions(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-directions",
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
    d: "M12 21v-4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 13v-4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 5v-2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10 21h4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 5v4h11l2 -2l-2 -2z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 13v4h-8l-2 -2l2 -2z"
  }));
}

export { Directions as default };
