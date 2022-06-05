import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function CoffeeOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-coffee-off",
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
    d: "M3 14c.83 .642 2.077 1.017 3.5 1c1.423 .017 2.67 -.358 3.5 -1c.73 -.565 1.783 -.923 2.994 -.99"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 3c-.194 .14 -.364 .305 -.506 .49"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 3a2.4 2.4 0 0 0 -1 2a2.4 2.4 0 0 0 1 2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 10h3v3m-.257 3.743a6.003 6.003 0 0 1 -5.743 4.257h-2a6 6 0 0 1 -6 -6v-5h7"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20.116 16.124a3 3 0 0 0 -3.118 -4.953"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { CoffeeOff as default };
